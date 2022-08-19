import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { IBook } from 'src/app/domain/ibook';
import { BooksService } from 'src/app/services/books.service';

@Component({
  selector: 'app-books-list',
  templateUrl: './books-list.component.html',
  styleUrls: ['./books-list.component.scss']
})
export class BooksListComponent implements OnInit {

  data : IBook[];

  constructor(private _service: BooksService,
              private _routing : ActivatedRoute) { }

  ngOnInit(): void {
        // Récupérer le id la categorie (snapshot)
        //const localId =  +this._routing.snapshot.paramMap.get('id');
        this._routing.paramMap.subscribe(
            res => {
                 const localId = +res.get('id');
                 this._service.getAllBooksForACategory(localId).subscribe(
                      innerResp => this.data = innerResp
                 );
            }

        );


  }

}
