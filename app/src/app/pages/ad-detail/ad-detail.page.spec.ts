import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdDetailPage } from './ad-detail.page';

describe('AdDetailPage', () => {
  let component: AdDetailPage;
  let fixture: ComponentFixture<AdDetailPage>;

  beforeEach(() => {
    fixture = TestBed.createComponent(AdDetailPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
