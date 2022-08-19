export interface IBook {

  id: number;
  title: string;
  authors: string;
  description: string;
  price: number;
  imageURL: string;
  isbn? : string; // Field optionnel

}
