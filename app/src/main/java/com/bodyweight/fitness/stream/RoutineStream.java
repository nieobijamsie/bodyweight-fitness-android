package com.bodyweight.fitness.stream;

import com.bodyweight.fitness.App;
import com.bodyweight.fitness.model.Exercise;
import com.bodyweight.fitness.model.json.JSONRoutine;
import com.bodyweight.fitness.model.persistence.Glacier;
import com.bodyweight.fitness.utils.PreferenceUtils;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

import com.bodyweight.fitness.R;
import com.bodyweight.fitness.model.Routine;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

public class RoutineStream {
    private static RoutineStream sInstance;

    private Routine mRoutine;
    private Exercise mExercise;

    private final PublishSubject<Routine> mRoutineSubject = PublishSubject.create();
    private final PublishSubject<Routine> mRoutineChangedSubject = PublishSubject.create();
    private final PublishSubject<Exercise> mExerciseSubject = PublishSubject.create();
    private final PublishSubject<Routine> mLevelChangedSubject = PublishSubject.create();

    private RoutineStream() {
        setRoutine(R.raw.bodyweight_fitness_recommended_routine);
    }

    public static RoutineStream getInstance() {
        if(sInstance == null) {
            sInstance = new RoutineStream();
        }

        return sInstance;
    }

    public void setRoutine(Routine routine) {
        mRoutine = routine;

        PreferenceUtils.getInstance().setDefaultRoutine(mRoutine.getRoutineId());

        mExercise = mRoutine.getLinkedExercises().get(0);

        mRoutineSubject.onNext(mRoutine);
        mExerciseSubject.onNext(mExercise);

        mRoutineChangedSubject.onNext(mRoutine);
    }

    private void setRoutine(int resource) {
        mRoutine = getRoutine(resource);

        setRoutine(mRoutine);
    }

    public Routine getRoutine() {
        return mRoutine;
    }

    public Routine getRoutine(int resource) {
        try {
            JSONRoutine jsonRoutine = new Gson().fromJson(IOUtils.toString(App.getContext()
                            .getResources()
                            .openRawResource(resource)
            ), JSONRoutine.class);

            return new Routine(jsonRoutine);
        } catch(IOException e) {}

        return null;
    }

    public void setExercise(Exercise exercise) {
        mExercise = exercise;
        mExerciseSubject.onNext(exercise);
    }

    public Exercise getExercise() {
        return mExercise;
    }

    public void setLevel(Exercise exercise, int level) {
        mRoutine.setLevel(exercise, level);

        setExercise(exercise);

        // We save our current exercise for given section
        Glacier.put(
                exercise.getSection().getSectionId(),
                exercise.getSection().getCurrentExercise().getExerciseId()
        );

        mLevelChangedSubject.onNext(mRoutine);
    }

    public Observable<Routine> getRoutineObservable() {

        Observable<Routine> routineObservable = Observable
                .just(mRoutine)
                .observeOn(AndroidSchedulers.mainThread())
                .publish()
                .refCount();

        return Observable.merge(mRoutineSubject, routineObservable);
    }

    public Observable<Routine> getRoutineChangedObservable() {
        return mRoutineChangedSubject;
    }

    public Observable<Exercise> getExerciseObservable() {
        Observable<Exercise> exerciseObservable = Observable
                .just(mExercise)
                .observeOn(AndroidSchedulers.mainThread())
                .publish()
                .refCount();

        return Observable.merge(mExerciseSubject, exerciseObservable);
    }
}
