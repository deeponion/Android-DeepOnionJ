package org.deeponion.walletTemplate.ui.website;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class WebsiteViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public WebsiteViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is slideshow fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}