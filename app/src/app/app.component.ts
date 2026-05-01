import { Component } from '@angular/core';
import { TabShellSyncService } from './services/tab-shell-sync.service';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  styleUrls: ['app.component.scss'],
  standalone: false,
})
export class AppComponent {
  /** Constructing the root tab shell sync service wires Router / resume / hash listeners. */
  constructor(_tabShellSync: TabShellSyncService) {}
}
