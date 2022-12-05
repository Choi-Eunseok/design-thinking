package com.example.ponyo;

public class animal {
    String name; //동물 이름
    String kind; //동물 종류

    public animal(){} //이건 기본적으로 쓰더라구요.


    //get, set 함수는 커스텀 리스트 뷰를 사용하시는 분들과.. 필요하신 분만 작성하시면 좋습니다.
    public String getname() {
        return name;
    }

    public void setname(String name) {
        this.name = name;
    }

    public String getkind() {
        return kind;
    }

    public void setkind(String kind) {
        this.kind = kind;
    }




    //값을 추가할때 쓰는 함수, MainActivity에서 addanimal함수에서 사용할 것임.
    public animal(String name, String kind){
        this.name = name;
        this.kind = kind;
    }
}