package org.deeponion.walletTemplate.ui.website;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import org.deeponion.walletTemplate.R;

public class WebsiteFragment extends Fragment {

    private WebsiteViewModel websiteViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        websiteViewModel =
                ViewModelProviders.of(this).get(WebsiteViewModel.class);
        View root = inflater.inflate(R.layout.fragment_website, container, false);
        WebView wv = root.findViewById(R.id.web_view);
        wv.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.loadUrl("https://deeponion.org");
        return root;
    }
}
