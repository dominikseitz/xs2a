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
 * PSU Data for Update PSU Authentication.
 */
@ApiModel(description = "PSU Data for Update PSU Authentication.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2019-08-07T16:04:49.625002+03:00[Europe/Kiev]")

public class PsuData   {
  @JsonProperty("password")
  private String password = null;

  @JsonProperty("encryptedPassword")
  private String encryptedPassword = null;

  @JsonProperty("additionalPassword")
  private String additionalPassword = null;

  @JsonProperty("additionalEncryptedPassword")
  private String additionalEncryptedPassword = null;

  public PsuData password(String password) {
    this.password = password;
    return this;
  }

  /**
   * Password
   * @return password
  **/
  @ApiModelProperty(value = "Password")



  @JsonProperty("password")
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public PsuData encryptedPassword(String encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
    return this;
  }

  /**
   * Encrypted password.
   * @return encryptedPassword
  **/
  @ApiModelProperty(value = "Encrypted password.")



  @JsonProperty("encryptedPassword")
  public String getEncryptedPassword() {
    return encryptedPassword;
  }

  public void setEncryptedPassword(String encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
  }

  public PsuData additionalPassword(String additionalPassword) {
    this.additionalPassword = additionalPassword;
    return this;
  }

  /**
   * Additional password in plaintext
   * @return additionalPassword
  **/
  @ApiModelProperty(value = "Additional password in plaintext")



  @JsonProperty("additionalPassword")
  public String getAdditionalPassword() {
    return additionalPassword;
  }

  public void setAdditionalPassword(String additionalPassword) {
    this.additionalPassword = additionalPassword;
  }

  public PsuData additionalEncryptedPassword(String additionalEncryptedPassword) {
    this.additionalEncryptedPassword = additionalEncryptedPassword;
    return this;
  }

  /**
   * Additional encrypted password
   * @return additionalEncryptedPassword
  **/
  @ApiModelProperty(value = "Additional encrypted password")



  @JsonProperty("additionalEncryptedPassword")
  public String getAdditionalEncryptedPassword() {
    return additionalEncryptedPassword;
  }

  public void setAdditionalEncryptedPassword(String additionalEncryptedPassword) {
    this.additionalEncryptedPassword = additionalEncryptedPassword;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PsuData psuData = (PsuData) o;
    return Objects.equals(this.password, psuData.password) &&
        Objects.equals(this.encryptedPassword, psuData.encryptedPassword) &&
        Objects.equals(this.additionalPassword, psuData.additionalPassword) &&
        Objects.equals(this.additionalEncryptedPassword, psuData.additionalEncryptedPassword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(password, encryptedPassword, additionalPassword, additionalEncryptedPassword);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PsuData {\n");
    
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    encryptedPassword: ").append(toIndentedString(encryptedPassword)).append("\n");
    sb.append("    additionalPassword: ").append(toIndentedString(additionalPassword)).append("\n");
    sb.append("    additionalEncryptedPassword: ").append(toIndentedString(additionalEncryptedPassword)).append("\n");
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

