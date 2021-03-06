package de.adorsys.psd2.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Account status. The value is one of the following:   - \"enabled\": account is available   - \"deleted\": account is terminated   - \"blocked\": account is blocked e.g. for legal reasons If this field is not used, than the account is available in the sense of this specification. 
 */
public enum AccountStatus {
  
  ENABLED("enabled"),
  
  DELETED("deleted"),
  
  BLOCKED("blocked");

  private String value;

  AccountStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static AccountStatus fromValue(String text) {
    for (AccountStatus b : AccountStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

