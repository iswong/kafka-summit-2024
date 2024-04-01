package ks2004.demo.model;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Data
@Builder(toBuilder = true)
public class ExposureSnapshot implements Serializable {

  private static final long serialVersionUID = 7687162387126L;

  private String parentAccount;
  private String accountCode;
  private List<Exposure> exposures;

  private double totalSodEmv;
  private double totalSodTmv;
  private double totalExpectedEmv;
  private double totalExpectedTmv;

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("parentAccount", parentAccount)
        .append("accountCode", accountCode)
        .append("totalSodEmv", totalSodEmv)
        .append("totalSodTmv", totalSodTmv)
        .append("totalExpectedEmv", totalExpectedEmv)
        .append("totalExpectedTmv", totalExpectedTmv)
        .toString();
  }

  @Data
  @Builder(toBuilder = true)
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

  }

}
