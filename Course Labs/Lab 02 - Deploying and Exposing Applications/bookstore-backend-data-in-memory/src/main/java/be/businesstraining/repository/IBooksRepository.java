package be.businesstraining.repository;

import be.businesstraining.entities.Book;

import java.util.List;
import java.util.Optional;

public interface IBooksRepository  {
	public Book addBook(Book a);
    public List<Book> findAll();
    public Optional<Book> findById(Long id );

}
