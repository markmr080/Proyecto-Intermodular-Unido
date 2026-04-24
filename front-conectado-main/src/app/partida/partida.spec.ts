import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Partida } from './partida';

describe('Partida', () => {
  let component: Partida;
  let fixture: ComponentFixture<Partida>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Partida]
    })
      .compileComponents();

    fixture = TestBed.createComponent(Partida);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
