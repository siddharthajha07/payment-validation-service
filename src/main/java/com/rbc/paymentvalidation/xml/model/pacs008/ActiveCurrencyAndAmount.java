package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import java.math.BigDecimal;

/**
 * A monetary amount with its currency, such as IntrBkSttlmAmt Ccy="BBD" of 1.02.
 *
 * BigDecimal, never double. Binary floating point cannot represent 1.02 exactly, so double
 * accumulates error under arithmetic and comparison. BigDecimal is exact and preserves the
 * scale as written, which is what lets the at-most-two-decimal-places rule be checked against
 * what the sender actually sent.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ActiveCurrencyAndAmount {

    @XmlValue
    private BigDecimal value;

    @XmlAttribute(name = "Ccy")
    private String currency;

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
