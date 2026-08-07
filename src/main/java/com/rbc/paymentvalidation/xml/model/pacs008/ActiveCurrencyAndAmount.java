package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import java.math.BigDecimal;

/**
 * A monetary amount with its currency, for example
 * {@code <IntrBkSttlmAmt Ccy="BBD">1.02</IntrBkSttlmAmt>}.
 *
 * <p>The amount is a {@link BigDecimal}, never a {@code double}. Binary floating point
 * cannot represent decimal fractions such as {@code 1.02} exactly, so {@code double}
 * accumulates error under arithmetic and comparison. {@code BigDecimal} also preserves
 * the scale as written, which is what allows the "at most two decimal places" rule to be
 * checked against what the sender actually sent.
 *
 * <p>{@code @XmlValue} binds the element's text content; {@code @XmlAttribute} binds the
 * {@code Ccy} attribute alongside it.
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
