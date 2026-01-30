import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { I18nPipe } from '../../core/i18n.pipe';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [I18nPipe, RouterLink],
  templateUrl: './not-found.component.html',
  styleUrls: ['./not-found.component.css']
})
export class NotFoundComponent {}
