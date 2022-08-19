package be.businesstraining;

import be.businesstraining.entities.Book;
import be.businesstraining.entities.Category;
import be.businesstraining.repository.IBooksRepository;
import be.businesstraining.repository.ICategoriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.util.Arrays;

@SpringBootApplication
public class BookstoreBackendApplication {

	private static final Logger log =
			LoggerFactory.getLogger(BookstoreBackendApplication.class);

	public static void main(String[] args) {

		SpringApplication.run(BookstoreBackendApplication.class, args);
	}
	@Bean
	public CommandLineRunner initData(ICategoriesRepository catRepo, IBooksRepository bookRepo) {
		return (args) -> {
			
			// Create Category "Programming"
						Category cat0 = new Category(null, "DevOps", null);
						catRepo.save(cat0);

						Book b01 = new Book(null, "Learning DevOps", " Mikael Krief", cat0,  "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://static.packt-cdn.com/products/9781838642730/cover/smaller" );
						Book b02 = new Book(null, "Docker in action, 2nd Edition", "Jeff Nickoloff and Stephen Kuenzli", cat0, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://drek4537l1klr.cloudfront.net/nickoloff2/Figures/cover.jpg" );
						Book b03 = new Book(null, "Kubernetes: Up and Running, 2nd Edition", "Brendan Burns, Joe Beda, Kelsey Hightower", cat0, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://covers.oreilly.com/images/0636920223788/cat.gif");
						Book b04 = new Book(null, "Continuous Delivery with Docker and Jenkins", "Rafal Leszko", cat0, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://images-na.ssl-images-amazon.com/images/I/41lPh+vZh2L._SX404_BO1,204,203,200_.jpg" );
					    bookRepo.saveAll(Arrays.asList(b01, b02, b03, b04));
			
			// Create Category "Programming"
			Category cat1 = new Category(null, "Programming", null);
			catRepo.save(cat1);

			Book b11 = new Book(null, "Java OCA", "Kathy Sierra", cat1, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://images-na.ssl-images-amazon.com/images/I/51ptF7BSDYL._SX403_BO1,204,203,200_.jpg" );
			Book b12 = new Book(null, "Spring In Action", "Craig Walls", cat1, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://learning.oreilly.com/library/cover/9781617294945/250w/" );
			Book b13 = new Book(null, "Angular In Action", "Jeremy Wilken", cat1, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://images-na.ssl-images-amazon.com/images/I/41Vs-83rPxL._SX258_BO1,204,203,200_.jpg");
			Book b14 = new Book(null, "GOF Design patterns", "The GOF", cat1, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://images-na.ssl-images-amazon.com/images/I/51kuc0iWoKL._SX326_BO1,204,203,200_.jpg" );
		    bookRepo.saveAll(Arrays.asList(b11, b12, b13, b14));
		    // Create Category "Litterature"
			Category cat3 = new Category(null, "Security", null);
			catRepo.save(cat3);

			Book b31 = new Book(null, "Hands-On Security in Devops", "Tony Hsiang-Chih Hsu", cat3, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://m.media-amazon.com/images/I/51gG+OsDaqL._SX260_.jpg" );
			Book b32 = new Book(null, "Web Application Security", "Andrew Hoffman", cat3, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://d1w7fb2mkkr3kw.cloudfront.net/assets/images/book/lrg/9781/4920/9781492053118.jpg" );
			Book b33 = new Book(null, "Kubernetes Security", "Kaizhe Huang, Pranjal Jumde", cat3, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://images-na.ssl-images-amazon.com/images/I/513cFVYyqmL._SX404_BO1,204,203,200_.jpg" );
			bookRepo.saveAll(Arrays.asList(b31, b32, b33));

		    // Create Category "Litterature"
			Category cat4 = new Category(null, "Litterature", null);
			catRepo.save(cat4);

			Book b41 = new Book(null, "Les misérables", "Victor Hugo", cat4, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://m.media-amazon.com/images/I/510ypkdwIYL.jpg" );
			Book b42 = new Book(null, "L'être et le néant", "Jean Paul Sartre", cat4, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://images-na.ssl-images-amazon.com/images/I/41MSHBns7WL._SX210_.jpg" );
			Book b43 = new Book(null, "L'étranger", "Albert Camus", cat4, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://static.fnac-static.com/multimedia/Images/FR/NR/1c/f9/01/129308/1540-0/tsp20191030070736/L-Etranger.jpg" );
			Book b44 = new Book(null, "Don Quichotte", "Miguel de Cervantes Saavedra", cat4, "Some description Lorem ipsum Lorem ipsum", BigDecimal.TEN, "https://static.fnac-static.com/multimedia/Images/FR/NR/92/eb/74/7662482/1540-1/tsp20201218074133/Don-Quichotte.jpg" );
			bookRepo.saveAll(Arrays.asList(b41, b42, b43, b44));

			// fetch all categories
			log.info("Categories found with findAll():");
			log.info("-------------------------------");
			for( Category category : catRepo.findAll()) {
				log.info(category.getTitle());
			}
			// fetch all categories
			log.info("Books found with findAll():");
			log.info("-------------------------------");
			for( Book book : bookRepo.findAll()) {
				log.info(book.getTitle());
			}
			// Test filtering by title or author
			log.info("Test Filering by title or author name");
			log.info("-------------------------------");
			for( Book book : bookRepo.findAllByTitleContainingIgnoreCaseOrAuthorsContainingIgnoreCase("P", "P")) {
				log.info("Title: " +book.getTitle() + "Authors: "+ book.getAuthors());
			}
		};
	}

}
