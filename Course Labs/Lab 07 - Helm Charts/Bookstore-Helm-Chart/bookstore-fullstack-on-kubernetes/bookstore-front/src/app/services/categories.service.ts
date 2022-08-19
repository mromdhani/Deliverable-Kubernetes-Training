import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { ICategory } from '../domain/icategory';

@Injectable({
  providedIn: 'root'
})
export class CategoriesService {


  constructor(private _http: HttpClient) { }   // Inject HttpClient

  public getAllCategories(): Observable<ICategory[]> {

    const URL: string = environment.url_base + '/categories';

    return this._http.get<ICategory[]>( URL );

  }
}
