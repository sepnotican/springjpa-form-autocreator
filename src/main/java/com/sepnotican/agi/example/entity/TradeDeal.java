package com.sepnotican.agi.example.entity;

import com.sepnotican.agi.core.annotations.AgiEntity;
import com.sepnotican.agi.core.annotations.LinkedObject;
import com.sepnotican.agi.core.annotations.RepresentationResolver;
import com.sepnotican.agi.core.annotations.Synonym;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "trade_deals")
@AgiEntity(menuCaption = "Trade deals",
        singleCaption = "Trade deal", fieldForInputSearch = "id",
        menuPath = "/Trade")
@Getter
@Setter
@NoArgsConstructor
@RepresentationResolver("representation")
public class TradeDeal {

    @Column
    @Synonym("Summ of the deal")
    double sum;

    @LinkedObject
    @Synonym("Customer")
    @ManyToOne
    @JoinColumn(name = "customer_id")
    Customer customer;
    @Id
    @GeneratedValue
    private Long id;

    public TradeDeal(double sum, Customer customer) {

        this.sum = sum;
        this.customer = customer;
    }

    public String representation() {
        return "" + getId() + ":" + getCustomer() + " $" + getSum();
    }

}
