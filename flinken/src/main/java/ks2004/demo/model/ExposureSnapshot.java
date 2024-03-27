package ks2004.demo.model;

import java.io.Serializable;
import java.util.List;

public class ExposureSnapshot implements Serializable {

    private static final long serialVersionUID = 7687162387126L;

    private String accountCode;
    private List<Exposure> exposures;

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public List<Exposure> getExposures() {
        return exposures;
    }

    public void setExposures(List<Exposure> exposures) {
        this.exposures = exposures;
    }

    public static class Exposure {
        private String symbol;

        private double sodQty;
        private double orderQty;
        private double sodEmv;
        private double sodTmv;
        private double expectedEmv;
        private double expectedTmv;

        private double sodEmvPercent;
        private double sodTmvPercent;
        private double expectedEmvPercent;
        private double expectedTmvPercent;

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

        public double getSodEmv() {
            return sodEmv;
        }

        public void setSodEmv(double sodEmv) {
            this.sodEmv = sodEmv;
        }

        public double getSodTmv() {
            return sodTmv;
        }

        public void setSodTmv(double sodTmv) {
            this.sodTmv = sodTmv;
        }

        public double getExpectedEmv() {
            return expectedEmv;
        }

        public void setExpectedEmv(double expectedEmv) {
            this.expectedEmv = expectedEmv;
        }

        public double getExpectedTmv() {
            return expectedTmv;
        }

        public void setExpectedTmv(double expectedTmv) {
            this.expectedTmv = expectedTmv;
        }

        public double getSodEmvPercent() {
            return sodEmvPercent;
        }

        public void setSodEmvPercent(double sodEmvPercent) {
            this.sodEmvPercent = sodEmvPercent;
        }

        public double getSodTmvPercent() {
            return sodTmvPercent;
        }

        public void setSodTmvPercent(double sodTmvPercent) {
            this.sodTmvPercent = sodTmvPercent;
        }

        public double getExpectedEmvPercent() {
            return expectedEmvPercent;
        }

        public void setExpectedEmvPercent(double expectedEmvPercent) {
            this.expectedEmvPercent = expectedEmvPercent;
        }

        public double getExpectedTmvPercent() {
            return expectedTmvPercent;
        }

        public void setExpectedTmvPercent(double expectedTmvPercent) {
            this.expectedTmvPercent = expectedTmvPercent;
        }
    }

}
