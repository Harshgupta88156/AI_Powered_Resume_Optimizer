import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

import { Navbar } from './components/navbar/navbar';
import { Hero } from './components/hero/hero';
import { Features } from './components/features/features';
import { Footer } from './components/footer/footer';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [
    CommonModule,
    Navbar,
    Hero,
    Features,
    Footer
  ],
  templateUrl: './landing.html',
  styleUrls: ['./landing.css']
})
export class Landing {

}
