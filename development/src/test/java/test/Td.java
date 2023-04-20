package test;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by omar on 4/28/17.
 */
public class Td {


  public static void main(String[] args) throws InterruptedException {

    Observable

        .create(new ObservableOnSubscribe<Object>() {
          @Override
          public void subscribe(ObservableEmitter<Object> e) throws Exception {
            System.out.println("aaa"+Thread.currentThread().getName());
            e.onNext("dada");
            e.onNext("dada11111");
            e.onNext("dada2222");
            e.onNext("dada333");
            e.onComplete();
            e.onError(new Exception("test error"));
          }
        }).doOnNext(x -> {
            System.out.println("x-> " + x);
            System.out.println(Thread.currentThread().getName());
        })
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.newThread())
        .subscribe(new Observer<Object>() {

          @Override
          public void onSubscribe(Disposable d) {
            System.out.println("on subs");
            System.out.println(Thread.currentThread().getName());
          }

          @Override
          public void onNext(Object o) {
            System.out.println(o);
            System.out.println(Thread.currentThread().getName());
          }

          @Override
          public void onError(Throwable e) {
            System.out.println(e);
            System.out.println(Thread.currentThread().getName());
          }

          @Override
          public void onComplete() {
            System.out.println("on complete");
            System.out.println(Thread.currentThread().getName());
          }
        });

//		Observable.just("long", "longer", "longest")
//				.flatMap(v ->
//						Observable.just(v).doOnNext(s -> System.out.println("processing item on thread " + Thread.currentThread().getName()))
//								.subscribeOn(Schedulers.computation()))
//				.subscribe(length -> System.out.println("received item length " + length + " on thread " + Thread.currentThread().getName()));
//
//
    Thread.sleep(5000);
  }
}
