package de.adorsys.psd2.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Content of the body of a Transaction Authorisation Request 
 */
@ApiModel(description = "Content of the body of a Transaction Authorisation Request ")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2019-08-07T16:04:49.625002+03:00[Europe/Kiev]")

public class TransactionAuthorisation   {
  @JsonProperty("scaAuthenticationData")
  private String scaAuthenticationData = null;

  public TransactionAuthorisation scaAuthenticationData(String scaAuthenticationData) {
    this.scaAuthenticationData = scaAuthenticationData;
    return this;
  }

  /**
   * Get scaAuthenticationData
   * @return scaAuthenticationData
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull



  @JsonProperty("scaAuthenticationData")
  public String getScaAuthenticationData() {
    return scaAuthenticationData;
  }

  public void setScaAuthenticationData(String scaAuthenticationData) {
    this.scaAuthenticationData = scaAuthenticationData;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransactionAuthorisation transactionAuthorisation = (TransactionAuthorisation) o;
    return Objects.equals(this.scaAuthenticationData, transactionAuthorisation.scaAuthenticationData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scaAuthenticationData);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TransactionAuthorisation {\n");
    
    sb.append("    scaAuthenticationData: ").append(toIndentedString(scaAuthenticationData)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

