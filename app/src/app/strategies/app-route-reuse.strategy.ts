import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  DetachedRouteHandle,
  RouteReuseStrategy,
} from '@angular/router';

/**
 * Default Angular-style reuse (no detached handles). Ionic's {@link IonicRouteStrategy}
 * caches tab routes so inactive tabs stay mounted as stacked ion-pages; on Android WebView
 * that stack can sit above the active tab and swallow touches while the tab bar still works.
 */
@Injectable()
export class AppRouteReuseStrategy implements RouteReuseStrategy {
  shouldDetach(): boolean {
    return false;
  }

  store(_route: ActivatedRouteSnapshot, _handle: DetachedRouteHandle | null): void {}

  shouldAttach(): boolean {
    return false;
  }

  retrieve(_route: ActivatedRouteSnapshot): DetachedRouteHandle | null {
    return null;
  }

  shouldReuseRoute(future: ActivatedRouteSnapshot, curr: ActivatedRouteSnapshot): boolean {
    return future.routeConfig === curr.routeConfig;
  }
}
