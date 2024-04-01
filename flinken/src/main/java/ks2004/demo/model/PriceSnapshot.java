package ks2004.demo.model;

import java.io.Serializable;
import java.util.Map;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Data
public class PriceSnapshot implements Serializable {

  private static final long serialVersionUID = 283947289341L;

  private String asOf;
  private Map<String, Double> prices;

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("asOf", asOf)
        .toString();
  }
}
