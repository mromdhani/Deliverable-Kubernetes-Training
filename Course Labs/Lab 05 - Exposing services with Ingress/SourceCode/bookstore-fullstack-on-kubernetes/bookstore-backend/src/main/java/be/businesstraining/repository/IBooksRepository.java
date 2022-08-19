package be.businesstraining.repository;

import be.businesstraining.entities.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IBooksRepository extends JpaRepository<Book,Long> {

    public List<Book>findAllByTitleContainingIgnoreCaseOrAuthorsContainingIgnoreCase(String title, String authors);

}
