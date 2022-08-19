package be.businesstraining.rest;

import be.businesstraining.entities.Book;
import be.businesstraining.entities.Category;
import be.businesstraining.repository.ICategoriesRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/categories")
@CrossOrigin(origins = "*")
public class CategoriesRestController {

    private ICategoriesRepository categoriesRepository;

    // The @Autowired decoration is not required for constructor injection
    public CategoriesRestController(ICategoriesRepository categoriesRepository) {
        this.categoriesRepository = categoriesRepository;
    }

    @GetMapping
    public List<Category> findAll() {
        return  categoriesRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> findById(@PathVariable Long id) {
        Optional<Category> category = categoriesRepository.findById(id);
        return  category.isPresent()?
                new ResponseEntity<>(category.get(), HttpStatus.OK):
                new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    @GetMapping(value = "/{id}/books")
    public ResponseEntity<List<Book>> findBooksForCategory(@PathVariable Long id) {
        Optional<Category> category = categoriesRepository.findById(id);
        if (category.isPresent()) {
             List<Book> books = category.get().getBooks();
             return (books !=null)?
                     new ResponseEntity<>(books, HttpStatus.OK):
                     new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        else return  new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
