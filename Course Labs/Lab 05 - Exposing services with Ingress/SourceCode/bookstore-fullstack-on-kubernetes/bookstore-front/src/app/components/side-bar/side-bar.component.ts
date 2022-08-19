import { Component, OnInit } from '@angular/core';
import { ICategory } from 'src/app/domain/icategory';
import { CategoriesService } from 'src/app/services/categories.service';

@Component({
  selector: 'app-side-bar',
  templateUrl: './side-bar.component.html',
  styleUrls: ['./side-bar.component.scss']
})
export class SideBarComponent implements OnInit {

  data : ICategory[];

  constructor(private _cat : CategoriesService) { }  // Inject CategoriesService

  ngOnInit(): void {
    this._cat.getAllCategories().subscribe(
      response => this.data = response,
      erreur => console.log('ATTENTION, Ily a eu  l\'erreur: '+ erreur)
    );
  }

}
