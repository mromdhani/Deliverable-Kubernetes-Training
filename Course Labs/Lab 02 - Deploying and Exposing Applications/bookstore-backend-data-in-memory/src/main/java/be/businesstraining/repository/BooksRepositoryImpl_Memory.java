package be.businesstraining.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import be.businesstraining.entities.Book;

@Repository
public class BooksRepositoryImpl_Memory implements IBooksRepository {

	private static List<Book>data = new ArrayList<Book>(Arrays.asList(
			new Book(01L, "Learning DevOps", " Mikael Krief", "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://static.packt-cdn.com/products/9781838642730/cover/smaller" ),
			new Book(02L, "Docker in action, 2nd Edition", "Jeff Nickoloff and Stephen Kuenzli",  "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://drek4537l1klr.cloudfront.net/nickoloff2/Figures/cover.jpg" ),
			new Book(03L, "Kubernetes: Up and Running, 2nd Edition", "Brendan Burns, Joe Beda, Kelsey Hightower",  "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://covers.oreilly.com/images/0636920223788/cat.gif"),
			new Book(04L, "Continuous Delivery with Docker and Jenkins", "Rafal Leszko",  "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://images-na.ssl-images-amazon.com/images/I/41lPh+vZh2L._SX404_BO1,204,203,200_.jpg" )
			));
				
	@Override
	public Book addBook(Book book) {		
		return  data.add(book) ? book : null;
	}

	@Override
	public List<Book> findAll() {
	    return data;
	}

	@Override
	public Optional<Book> findById(Long id) {
		return data.stream().filter(b ->b.getId().equals(id)).findFirst();
	}

}
