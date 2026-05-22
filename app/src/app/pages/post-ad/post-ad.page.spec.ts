import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { IonicModule } from '@ionic/angular';
import { PostAdPage } from './post-ad.page';
import { PostsService } from '../../services/posts.service';

describe('PostAdPage', () => {
  let component: PostAdPage;
  let fixture: ComponentFixture<PostAdPage>;

  beforeEach(async () => {
    const postsServiceMock = {};

    await TestBed.configureTestingModule({
      declarations: [PostAdPage],
      imports: [
        IonicModule.forRoot(),
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        RouterTestingModule
      ],
      providers: [
        { provide: PostsService, useValue: postsServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PostAdPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
