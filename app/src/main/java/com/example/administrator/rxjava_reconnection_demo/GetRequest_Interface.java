package com.example.administrator.rxjava_reconnection_demo;


import io.reactivex.Observable;
import retrofit2.http.GET;

public interface GetRequest_Interface {
    @GET("ajax.php?a=fy&f=auto&t=auto&w=shit%20holly")
      Observable<Translation> getCall();
}
