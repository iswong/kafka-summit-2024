package ks2004.demo.model;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Map;

public class PriceSnapshot implements Serializable {

    private static final long serialVersionUID = 283947289341L;

    private String asOf;
    private Map<String, Double> prices;

    public String getAsOf() {
        return asOf;
    }

    public void setAsOf(String asOf) {
        this.asOf = asOf;
    }

    public Map<String, Double> getPrices() {
        return prices;
    }

    public void setPrices(Map<String, Double> prices) {
        this.prices = prices;
    }
}
