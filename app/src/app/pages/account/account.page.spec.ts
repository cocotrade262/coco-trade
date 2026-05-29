import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { AccountPage } from './account.page';
import { PostsService } from '../../services/posts.service';
import { AuthService } from '../../services/auth.service';
import { of } from 'rxjs';

describe('AccountPage', () => {
  let component: AccountPage;
  let fixture: ComponentFixture<AccountPage>;

  beforeEach(async () => {
    const postsServiceMock = {
      getUserPosts: jasmine.createSpy('getUserPosts').and.returnValue(of([])),
      allPosts$: of([]),
      posts$: of([])
    };
    const authServiceMock = {
      user$: of(null),
      logout: jasmine.createSpy('logout')
    };

    await TestBed.configureTestingModule({
      declarations: [AccountPage],
      imports: [IonicModule.forRoot(), CommonModule, FormsModule],
      providers: [
        { provide: PostsService, useValue: postsServiceMock },
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AccountPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
