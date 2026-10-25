package com.inventory.negosyopro.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.inventory.negosyopro.dao.BusinessDAO;
import com.inventory.negosyopro.dao.ProductSuppliersDAO;
import com.inventory.negosyopro.dao.ProductsDAO;
import com.inventory.negosyopro.dao.PurchaseOrdersDAO;
import com.inventory.negosyopro.dao.ReportsDAO;
import com.inventory.negosyopro.dao.SuppliersDAO;
import com.inventory.negosyopro.dao.UsersDAO;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.Business;
import com.inventory.negosyopro.model.ProductSuppliers;
import com.inventory.negosyopro.model.Products;
import com.inventory.negosyopro.model.PurchaseOrderItems;
import com.inventory.negosyopro.model.PurchaseOrders;
import com.inventory.negosyopro.model.ReceivedItems;
import com.inventory.negosyopro.model.RestockSuggestions;
import com.inventory.negosyopro.model.Users;

public class PurchaseOrderService {
    private final PurchaseOrdersDAO purchaseOrdersDAO = new PurchaseOrdersDAO();
    private final SuppliersDAO suppliersDAO = new SuppliersDAO();
    private final ProductsDAO productsDAO = new ProductsDAO();
    private final ProductSuppliersDAO productSuppliersDAO = new ProductSuppliersDAO();
    private final BusinessDAO businessDAO = new BusinessDAO();
    private final ReportsDAO reportsDAO = new ReportsDAO();
    private final UsersDAO usersDAO = new UsersDAO();
    private final InventoryService inventoryService = new InventoryService();

    // Restock suggestion settings (change here if you want different assumptions).
    private static final int DEMAND_WINDOW_DAYS = 90;   // sales history used to measure demand
    private static final int DEFAULT_LEAD_TIME_DAYS = 7; // when no preferred supplier lead time is set
    private static final int SAFETY_STOCK_DAYS = 2;      // extra days of demand kept as a buffer

    // ------------------------------------------------------------------
    // Saves the header and its lines as DRAFT, with a generated po_number.
    // order supplies: businessId, supplierId, userId, expectedArrival, notes.
    // items supply: productId, quantityOrdered, unitCost (null = the product's cost price).
    // ------------------------------------------------------------------
    public long createPurchaseOrder(PurchaseOrders order, List<PurchaseOrderItems> items) throws SQLException {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("A purchase order needs at least one item.");
        }
        long businessId = order.getBusinessId();
        if (suppliersDAO.getSupplierById(order.getSupplierId(), businessId, true) == null) {
            throw new IllegalArgumentException("That supplier does not exist or is inactive.");
        }

        // Check every line before touching the database.
        List<PurchaseOrderItems> lines = new ArrayList<>();
        for (PurchaseOrderItems item : items) {
            if (item.getQuantityOrdered() <= 0) {
                throw new IllegalArgumentException("Every quantity must be more than zero.");
            }
            Products product = productsDAO.getProductById(item.getProductId(), businessId, true);
            if (product == null) {
                throw new IllegalArgumentException("Product " + item.getProductId() + " does not exist.");
            }
            BigDecimal cost = item.getUnitCost() != null ? item.getUnitCost() : product.getCostPrice();
            lines.add(new PurchaseOrderItems(0, 0, item.getProductId(), item.getQuantityOrdered(), 0, cost));
        }

        String poNumber = purchaseOrdersDAO.nextPoNumber(businessId);
        PurchaseOrders header = new PurchaseOrders(0, businessId, order.getSupplierId(), order.getUserId(),
                poNumber, PurchaseOrders.Status.DRAFT, null, order.getExpectedArrival(), null,
                order.getNotes(), null);

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long purchaseOrderId = purchaseOrdersDAO.insertPurchaseOrder(conn, header);
                for (PurchaseOrderItems line : lines) {
                    purchaseOrdersDAO.insertPurchaseOrderItem(conn, new PurchaseOrderItems(0, purchaseOrderId,
                            line.getProductId(), line.getQuantityOrdered(), 0, line.getUnitCost()));
                }
                conn.commit();
                return purchaseOrderId;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // DRAFT -> ORDERED (also sets ordered_at). false if the order is missing or not a draft.
    public boolean markOrdered(long purchaseOrderId, long businessId) throws SQLException {
        PurchaseOrders po = purchaseOrdersDAO.getPurchaseOrderById(purchaseOrderId, businessId);
        if (po == null || po.getStatus() != PurchaseOrders.Status.DRAFT) {
            return false;
        }
        return changeStatus(purchaseOrderId, businessId, PurchaseOrders.Status.ORDERED);
    }

    // Cancels an order that has not been received. false if missing or already received/cancelled.
    public boolean cancelPurchaseOrder(long purchaseOrderId, long businessId) throws SQLException {
        PurchaseOrders po = purchaseOrdersDAO.getPurchaseOrderById(purchaseOrderId, businessId);
        if (po == null || po.getStatus() == PurchaseOrders.Status.RECEIVED
                || po.getStatus() == PurchaseOrders.Status.CANCELLED) {
            return false;
        }
        return changeStatus(purchaseOrderId, businessId, PurchaseOrders.Status.CANCELLED);
    }

    // ------------------------------------------------------------------
    // Confirms a delivery in ONE transaction: records what arrived on each line, creates the
    // batches and RESTOCK rows, updates stock, and sets the order RECEIVED.
    // ------------------------------------------------------------------
    public void receivePurchaseOrder(long purchaseOrderId, long userId, List<ReceivedItems> received)
            throws SQLException {
        Users user = usersDAO.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user.");
        }
        long businessId = user.getBusinessId();

        PurchaseOrders po = purchaseOrdersDAO.getPurchaseOrderById(purchaseOrderId, businessId);
        if (po == null) {
            throw new IllegalArgumentException("That purchase order does not exist.");
        }
        if (po.getStatus() != PurchaseOrders.Status.ORDERED) {
            throw new IllegalStateException("Only an ORDERED purchase order can be received (this one is "
                    + po.getStatus() + ").");
        }
        if (received == null || received.isEmpty()) {
            throw new IllegalArgumentException("Nothing was entered as received.");
        }

        Map<Long, PurchaseOrderItems> itemsById = new HashMap<>();
        for (PurchaseOrderItems item : purchaseOrdersDAO.getItemsByPurchaseOrder(purchaseOrderId, businessId)) {
            itemsById.put(item.getPurchaseOrderItemId(), item);
        }

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (ReceivedItems r : received) {
                    PurchaseOrderItems item = itemsById.get(r.getPurchaseOrderItemId());
                    if (item == null) {
                        throw new IllegalArgumentException("Item " + r.getPurchaseOrderItemId()
                                + " is not part of this purchase order.");
                    }
                    if (r.getQuantityReceived() < 0) {
                        throw new IllegalArgumentException("Received quantity cannot be negative.");
                    }
                    if (r.getQuantityReceived() == 0) {
                        continue; // nothing arrived for this line
                    }
                    purchaseOrdersDAO.updateItemQuantityReceived(conn, item.getPurchaseOrderItemId(),
                            businessId, r.getQuantityReceived());
                    inventoryService.receiveStock(conn, businessId, item.getProductId(), userId,
                            r.getQuantityReceived(), item.getUnitCost(), r.getExpirationDate(),
                            po.getSupplierId(), item.getPurchaseOrderItemId(), purchaseOrderId,
                            "Received " + po.getPoNumber());
                }
                purchaseOrdersDAO.updateStatus(conn, purchaseOrderId, businessId, PurchaseOrders.Status.RECEIVED);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------
    // Reorder point and EOQ per product. Empty list while ordering_cost or holding_cost is missing.
    //   daily demand  d = units sold in the last 90 days / 90
    //   annual demand D = d x 365
    //   EOQ           = sqrt(2 x D x orderingCost / (holdingCost% x product cost))
    //   reorder point = d x (lead time + safety days)
    // Only products at or below their reorder point are suggested.
    // ------------------------------------------------------------------
    public List<RestockSuggestions> getRestockSuggestions(long businessId) throws SQLException {
        List<RestockSuggestions> suggestions = new ArrayList<>();

        Business business = businessDAO.getBusinessById(businessId);
        if (business == null || !isPositive(business.getOrderingCost()) || !isPositive(business.getHoldingCost())) {
            return suggestions;
        }
        double orderingCost = business.getOrderingCost().doubleValue();
        double holdingRate = business.getHoldingCost().doubleValue() / 100.0; // percent of cost per year

        for (Products product : productsDAO.getProductsByBusiness(businessId, true)) {
            if (!product.isTrackInventory() || !isPositive(product.getCostPrice())) {
                continue;
            }
            int unitsSold = reportsDAO.getUnitsSold(product.getProductId(), DEMAND_WINDOW_DAYS);
            if (unitsSold <= 0) {
                continue; // no sales, nothing to base a suggestion on
            }
            double dailyDemand = unitsSold / (double) DEMAND_WINDOW_DAYS;
            double annualDemand = dailyDemand * 365.0;

            ProductSuppliers preferred = productSuppliersDAO.getPreferredSupplier(product.getProductId(), businessId);
            int leadTime = (preferred != null && preferred.getLeadTimeDays() != null)
                    ? preferred.getLeadTimeDays() : DEFAULT_LEAD_TIME_DAYS;

            int reorderPoint = (int) Math.ceil(dailyDemand * (leadTime + SAFETY_STOCK_DAYS));
            if (product.getCurrentStock() > reorderPoint) {
                continue; // stock is still fine
            }

            double holdingPerUnit = holdingRate * product.getCostPrice().doubleValue();
            int eoq = (int) Math.ceil(Math.sqrt(2.0 * annualDemand * orderingCost / holdingPerUnit));

            suggestions.add(new RestockSuggestions(product, reorderPoint, Math.max(eoq, 1),
                    preferred != null ? preferred.getSupplierId() : null));
        }
        return suggestions;
    }

    // ---------------------------- helpers ----------------------------
    private boolean changeStatus(long purchaseOrderId, long businessId, PurchaseOrders.Status status)
            throws SQLException {
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean changed = purchaseOrdersDAO.updateStatus(conn, purchaseOrderId, businessId, status);
                conn.commit();
                return changed;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
