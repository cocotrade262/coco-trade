import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TabsPage } from './tabs.page';

/**
 * One routed shell + `:tab` param. Tab bodies are embedded in `tabs.page.html` (*ngIf),
 * not lazy-loaded as sibling ion-router-outlet routes — that stack left invisible ion-pages
 * on Android WebView and swallowed taps above the tab bar.
 */
const routes: Routes = [
  { path: '', redirectTo: 'feed', pathMatch: 'full' },
  {
    path: ':tab',
    component: TabsPage,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class TabsPageRoutingModule {}
