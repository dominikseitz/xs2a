/*
 * Copyright 2018-2019 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.psd2.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import de.adorsys.psd2.model.HrefType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * LinksAccountReport
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2019-09-16T11:06:46.430+02:00[Europe/Berlin]")

public class LinksAccountReport extends HashMap<String, HrefType>  {
  @JsonProperty("account")
  private HrefType account = null;

  @JsonProperty("first")
  private HrefType first = null;

  @JsonProperty("next")
  private HrefType next = null;

  @JsonProperty("previous")
  private HrefType previous = null;

  @JsonProperty("last")
  private HrefType last = null;

  public LinksAccountReport account(HrefType account) {
    this.account = account;
    return this;
  }

  /**
   * Get account
   * @return account
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid


  @JsonProperty("account")
  public HrefType getAccount() {
    return account;
  }

  public void setAccount(HrefType account) {
    this.account = account;
  }

  public LinksAccountReport first(HrefType first) {
    this.first = first;
    return this;
  }

  /**
   * Get first
   * @return first
  **/
  @ApiModelProperty(value = "")

  @Valid


  @JsonProperty("first")
  public HrefType getFirst() {
    return first;
  }

  public void setFirst(HrefType first) {
    this.first = first;
  }

  public LinksAccountReport next(HrefType next) {
    this.next = next;
    return this;
  }

  /**
   * Get next
   * @return next
  **/
  @ApiModelProperty(value = "")

  @Valid


  @JsonProperty("next")
  public HrefType getNext() {
    return next;
  }

  public void setNext(HrefType next) {
    this.next = next;
  }

  public LinksAccountReport previous(HrefType previous) {
    this.previous = previous;
    return this;
  }

  /**
   * Get previous
   * @return previous
  **/
  @ApiModelProperty(value = "")

  @Valid


  @JsonProperty("previous")
  public HrefType getPrevious() {
    return previous;
  }

  public void setPrevious(HrefType previous) {
    this.previous = previous;
  }

  public LinksAccountReport last(HrefType last) {
    this.last = last;
    return this;
  }

  /**
   * Get last
   * @return last
  **/
  @ApiModelProperty(value = "")

  @Valid


  @JsonProperty("last")
  public HrefType getLast() {
    return last;
  }

  public void setLast(HrefType last) {
    this.last = last;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    LinksAccountReport _linksAccountReport = (LinksAccountReport) o;
    return Objects.equals(this.account, _linksAccountReport.account) &&
        Objects.equals(this.first, _linksAccountReport.first) &&
        Objects.equals(this.next, _linksAccountReport.next) &&
        Objects.equals(this.previous, _linksAccountReport.previous) &&
        Objects.equals(this.last, _linksAccountReport.last);
  }

  @Override
  public int hashCode() {
    return Objects.hash(account, first, next, previous, last, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LinksAccountReport {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    account: ").append(toIndentedString(account)).append("\n");
    sb.append("    first: ").append(toIndentedString(first)).append("\n");
    sb.append("    next: ").append(toIndentedString(next)).append("\n");
    sb.append("    previous: ").append(toIndentedString(previous)).append("\n");
    sb.append("    last: ").append(toIndentedString(last)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

