package ks2004.demo.model;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;

public class HoldingSnapshot implements Serializable {

    private static final long serialVersionUID = 23402673489L;

    private String accountCode;
    private List<Holding> holdings;

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public List<Holding> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<Holding> holdings) {
        this.holdings = holdings;
    }

    @Override
    public String toString() {
        return "HoldingSnapshot{" +
                "accountCode='" + accountCode + '\'' +
                '}';
    }

    public static class Holding {
        private String symbol;
        private double sodQty;
        private double orderQty;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public double getSodQty() {
            return sodQty;
        }

        public void setSodQty(double sodQty) {
            this.sodQty = sodQty;
        }

        public double getOrderQty() {
            return orderQty;
        }

        public void setOrderQty(double orderQty) {
            this.orderQty = orderQty;
        }
    }

}
