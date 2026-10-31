package org.example.hazle;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Simple e-commerce product for the {@code hazle} package demo.
 *
 * <p>The class name is intentionally a slightly modified version of the
 * existing e-commerce {@code Product} class. It is stored in Hazelcast in
 * serialized form, so it implements {@link Serializable}.</p>
 */
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "hazleProductCache")
@Entity
@Table(name = "hazle_product")
public class HazleProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    public HazleProduct() {
    }

    public HazleProduct(String name, String category, BigDecimal price, int stock) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    @Override
    public String toString() {
        return "HazleProduct{id=" + id
                + ", name='" + name + '\''
                + ", category='" + category + '\''
                + ", price=" + price
                + ", stock=" + stock
                + '}';
    }
}
