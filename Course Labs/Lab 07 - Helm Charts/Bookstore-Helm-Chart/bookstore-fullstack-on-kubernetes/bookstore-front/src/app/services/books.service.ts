import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { IBook } from '../domain/ibook';

@Injectable({
  providedIn: 'root'
})
export class BooksService {


  constructor(private _http: HttpClient) { }  // inject HttpClient

  public getAllBooks() : Observable<IBook[]> {

    const URL: string = environment.url_base + '/books';

    return this._http.get<IBook[]>(URL);
  }

  public getAllBooksForACategory(idCat: number) : Observable<IBook[]> {

    const URL: string = environment.url_base + `/categories/${idCat}/books`;
     console.log(" ====== URL : "+ URL);
    return this._http.get<IBook[]>(URL);
  }

}
