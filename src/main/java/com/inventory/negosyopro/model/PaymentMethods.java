
package com.inventory.negosyopro.model;

public class PaymentMethods {
    private long paymentMethodId;
    private long businessId;
    private String methodName;
    private boolean isActive;

    public PaymentMethods(long paymentMethodId, long businessId, String methodName, boolean isActive) {
        this.paymentMethodId = paymentMethodId;
        this.businessId = businessId;
        this.methodName = methodName;
        this.isActive = isActive;
    }
    
    public long getPaymentMethodId() {
        return paymentMethodId;
    }

    public long getBusinessId() {
        return businessId;
    }

    public String getMethodName() {
        return methodName;
    }

    public boolean isActive() {
        return isActive;
    }
}
