import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-tabs',
  templateUrl: './tabs.page.html',
  styleUrls: ['./tabs.page.scss'],
  standalone: false,
})
export class TabsPage {
  constructor(private readonly router: Router) {}

  go(url: string, ev?: Event) {
    ev?.preventDefault();
    ev?.stopPropagation();
    void this.router.navigateByUrl(url);
  }
}
