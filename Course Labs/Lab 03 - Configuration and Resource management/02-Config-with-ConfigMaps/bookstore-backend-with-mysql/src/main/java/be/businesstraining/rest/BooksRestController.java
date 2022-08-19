package be.businesstraining.rest;

import be.businesstraining.entities.Book;
import be.businesstraining.entities.Category;
import be.businesstraining.repository.IBooksRepository;
import be.businesstraining.repository.ICategoriesRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/books")
@CrossOrigin
public class BooksRestController {

    private IBooksRepository booksRepository;
    private ICategoriesRepository categoriesRepository;

    // The @Autowired decoration is not required for constructor injection
    public BooksRestController(IBooksRepository booksRepository,
                              ICategoriesRepository categoriesRepository){
        this.booksRepository = booksRepository;
        this.categoriesRepository = categoriesRepository;
    }

    @GetMapping
    public List<Book> findAll() {
        return  booksRepository.findAll();
    }

    @GetMapping(params= {"numPage", "pageSize"})
    public Page<Book> findAll(@RequestParam (value = "numPage") int numPage,
                              @RequestParam(value = "pageSize",
                                            defaultValue = "2", required = false) int pageSize) {

        return  booksRepository.findAll(PageRequest.of(numPage, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> findById(@PathVariable Long id) {
        Optional<Book> book = booksRepository.findById(id);
        return  (book.isPresent()) ?
                new ResponseEntity<>(book.get(), HttpStatus.OK):
                new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping (params = "filter")
    public List<Book> findBooksFilteredByTitleOrAuthors(@PathParam(value = "filter") String filter) {
        return  booksRepository.
                findAllByTitleContainingIgnoreCaseOrAuthorsContainingIgnoreCase(filter,filter);
    }

    @PostMapping (path = "/category/{catId}")
    public ResponseEntity<Book> addBook(@PathVariable Long catId,
                                        @RequestBody Book book) {

       Optional<Category> category = categoriesRepository.findById(catId);
       if (! category.isPresent())
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
            else {
                book.setCategory(category.get());
                Book result = booksRepository.save(book);
                return (result != null)?
                        new ResponseEntity<Book>(result, HttpStatus.OK)
                        : new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
    }
}
