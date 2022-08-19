package be.businesstraining.entities;


import java.math.BigDecimal;


public class Book {
 
    private Long id;
    private String title;
    private String authors;
    private String description;
    private BigDecimal price;
    private String imageURL;

    public Book() {
    }

    public Book(Long id, String title, String authors,  String description, BigDecimal price, String imageURL) {
        this.id = id;
        this.title = title;
        this.authors = authors;     
        this.description = description;
        this.price = price;
        this.imageURL = imageURL;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

  
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }
}
