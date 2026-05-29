import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { IonicModule } from '@ionic/angular';
import { TabsPage } from './tabs.page';
import { AuthService } from '../../services/auth.service';
import { of } from 'rxjs';

describe('TabsPage', () => {
  let component: TabsPage;
  let fixture: ComponentFixture<TabsPage>;

  beforeEach(async () => {
    const authServiceMock = {
      user$: of(null)
    };

    await TestBed.configureTestingModule({
      declarations: [TabsPage],
      imports: [IonicModule.forRoot(), CommonModule, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TabsPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
