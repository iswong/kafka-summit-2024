package ks2004.demo.model;


import java.io.Serializable;
import java.util.List;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Data
public class HoldingSnapshot implements Serializable {

  private static final long serialVersionUID = 23402673489L;

  private String parentAccount;
  private String accountCode;
  private List<Holding> holdings;

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("parentAccount", parentAccount)
        .append("accountCode", accountCode)
        .toString();
  }

  @Data
  public static class Holding {

    private String symbol;
    private double sodQty;
    private double orderQty;
  }

}
