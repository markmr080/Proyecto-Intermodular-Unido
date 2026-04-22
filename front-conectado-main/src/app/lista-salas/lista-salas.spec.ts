import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListaSalas } from './lista-salas';

describe('ListaSalas', () => {
  let component: ListaSalas;
  let fixture: ComponentFixture<ListaSalas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListaSalas]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListaSalas);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
