package com.snapby.android.fragments;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.snapby.android.R;
import com.snapby.android.activities.CommentsActivity;
import com.snapby.android.activities.DisplayActivity;
import com.snapby.android.activities.MainActivity;
import com.snapby.android.models.Snapby;
import com.snapby.android.utils.ApiUtils;
import com.snapby.android.utils.Constants;
import com.snapby.android.utils.GeneralUtils;
import org.json.JSONObject;

/**
 * Created by bastien on 3/3/14.
 */
public class SnapbyPageFragment extends Fragment {

    private Snapby snapby = null;

    private AQuery aq = null;

    private ImageView imageView = null;

    private TextView usernameView = null;

    private ImageView userProfilePic = null;

    private LinearLayout userContainer = null;

    private View snapbyInfoContainer = null;

    private View likeContainer = null;

    private ImageView likeIcon = null;

    private TextView likeCount = null;

    private TextView userlikedCount = null;

    private View commentContainer = null;

    private TextView commentCount = null;

    private View imageContainer = null;

    private boolean liked = false;

    private MainActivity mainActivity = null;

    private String type = null;

    private View rootView = null;

    private int index = 0;

    public static SnapbyPageFragment newInstance(Snapby snapby, int index) {
        SnapbyPageFragment snapbyPageFragment = new SnapbyPageFragment();

        Bundle args = new Bundle();
        args.putParcelable("snapby", snapby);
        args.putInt("index", index);
        snapbyPageFragment.setArguments(args);

        return snapbyPageFragment;
    }

    public static SnapbyPageFragment newInstance(Snapby snapby, String type, int index) {
        SnapbyPageFragment snapbyPageFragment = new SnapbyPageFragment();

        Bundle args = new Bundle();
        args.putParcelable("snapby", snapby);
        args.putString("type", type);
        args.putInt("index", index);
        snapbyPageFragment.setArguments(args);

        return snapbyPageFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        aq = GeneralUtils.getAquery(mainActivity);

        index = getArguments().getInt("index");

        if ((getArguments().getString("type") != null && getArguments().getString("type").equals("profile"))) {
            type = "profile";
        } else {
            type = "explore";
        }

        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.snapby_page_fragment, container, false);
        this.rootView = rootView;
        snapby = getArguments().getParcelable("snapby");

        imageView = (ImageView) rootView.findViewById(R.id.explore_snapby_image);
        usernameView = (TextView) rootView.findViewById(R.id.explore_snapby_username);
        userProfilePic = (ImageView) rootView.findViewById(R.id.explore_snapby_user_picture);
        userContainer = (LinearLayout) rootView.findViewById(R.id.explore_snapby_user_container);
        imageContainer = rootView.findViewById(R.id.snapby_image_view_container);
        snapbyInfoContainer = rootView.findViewById(R.id.explore_snapby_info_container);
        likeContainer = rootView.findViewById(R.id.explore_snapby_like_container);
        likeIcon = (ImageView) rootView.findViewById(R.id.explore_snapby_like_button);
        likeCount = (TextView) rootView.findViewById(R.id.explore_snapby_like_count);
        commentContainer = rootView.findViewById(R.id.explore_snapby_comment_container);
        commentCount = (TextView) rootView.findViewById(R.id.explore_snapby_comment_count);
        userlikedCount = (TextView) rootView.findViewById(R.id.explore_snapby_liked_count);

        if (Constants.PRODUCTION) {
            aq.id(imageView).image(Constants.SMALL_SHOUT_IMAGE_URL_PREFIX_PROD + snapby.id + "--400", true, false, 0, 0, null, AQuery.FADE_IN);
        } else {
            aq.id(imageView).image(Constants.SMALL_SHOUT_IMAGE_URL_PREFIX_DEV + snapby.id + "--400", true, false, 0, 0, null, AQuery.FADE_IN);
        }

        if (type.equals("profile")) {
            userContainer.setVisibility(View.GONE);
        } else {
            userContainer.setVisibility(View.VISIBLE);
            imageContainer.setVisibility(View.VISIBLE);


            if (snapby.anonymous) {
                userProfilePic.setVisibility(View.GONE);
                usernameView.setText(getActivity().getString(R.string.anonymous_name));
                userlikedCount.setVisibility(View.GONE);
            } else {
                userProfilePic.setVisibility(View.VISIBLE);
                userlikedCount.setVisibility(View.VISIBLE);
                aq.id(userProfilePic).image(GeneralUtils.getProfileThumbPicturePrefix() + snapby.userId, true, false, 0, 0, null, AQuery.FADE_IN);
                usernameView.setText(snapby.username);
                userlikedCount.setText("(" + snapby.userScore + ")");
            }
        }

        setLikeCountUI(snapby.likeCount);
        setCommentCountUI(snapby.commentCount);

        if (mainActivity.myLikes.contains(snapby.id)) {
            liked = true;
            likeIcon.setImageDrawable(getResources().getDrawable(R.drawable.explore_snapby_like_button_liked));
        } else {
            liked = false;
            likeIcon.setImageDrawable(getResources().getDrawable(R.drawable.explore_snapby_like_button));
        }

        likeContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snapbyInfoContainer.setEnabled(false);

                if (liked) {
                    unlikeSnapby();

                    ApiUtils.removeLike(mainActivity, snapby.id, new AjaxCallback<JSONObject>() {
                        @Override
                        public void callback(String url, JSONObject object, AjaxStatus status) {
                            super.callback(url, object, status);

                            snapbyInfoContainer.setEnabled(true);

                            if (status.getError() != null) {
                                Toast toast = Toast.makeText(mainActivity, getString(R.string.snapby_unlike_failed), Toast.LENGTH_SHORT);
                                toast.show();

                                likeSnapby();
                            }
                        }
                    });
                } else {
                    likeSnapby();

                    ApiUtils.createLike(mainActivity, snapby, 0, 0, new AjaxCallback<JSONObject>() {
                        @Override
                        public void callback(String url, JSONObject object, AjaxStatus status) {
                            super.callback(url, object, status);

                            snapbyInfoContainer.setEnabled(true);

                            if (status.getError() != null) {
                                Toast toast = Toast.makeText(mainActivity, getString(R.string.snapby_like_failed), Toast.LENGTH_SHORT);
                                toast.show();

                                unlikeSnapby();
                            }
                        }
                    });
                }
            }
        });

        commentContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent comments = new Intent(getActivity(), CommentsActivity.class);
                comments.putExtra("snapby", snapby);

                Location myLocation = ((MainActivity) getActivity()).myLocation;

                if (myLocation != null && myLocation.getLatitude() != 0&& myLocation.getLongitude() != 0) {
                    comments.putExtra("myLocation", myLocation);
                }

                startActivityForResult(comments, Constants.COMMENTS_REQUEST);
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent displaySnapby = new Intent(getActivity(), DisplayActivity.class);
                displaySnapby.putExtra("snapby", snapby);

                startActivityForResult(displaySnapby, Constants.DISPLAY_SHOUT_REQUEST);
            }
        });

        if (index != 0) {
            disableSnapbyOptions();
        }

        return rootView;
    }

    private void likeSnapby() {
        liked = true;
        likeIcon.setImageDrawable(getResources().getDrawable(R.drawable.explore_snapby_like_button_liked));
        mainActivity.myLikes.add(snapby.id);

        if (type.equals("profile")) {
            mainActivity.reloadExploreSnapbies();
        } else {
            mainActivity.reloadProfileSnapbies();
        }

        snapby.likeCount++;
        setLikeCountUI(snapby.likeCount);
    }

    private void unlikeSnapby() {
        liked = false;
        likeIcon.setImageDrawable(getResources().getDrawable(R.drawable.explore_snapby_like_button));
        mainActivity.myLikes.remove(snapby.id);

        if (type.equals("profile")) {
            mainActivity.reloadExploreSnapbies();
        } else {
            mainActivity.reloadProfileSnapbies();
        }

        snapby.likeCount--;
        setLikeCountUI(snapby.likeCount);
    }

    private void setLikeCountUI(int count) {
        if (count > 1) {
            likeCount.setText(Integer.toString(count));
        } else {
            likeCount.setText(Integer.toString(count));
        }
    }

    private void setCommentCountUI(int count) {
        if (count > 1) {
            commentCount.setText(Integer.toString(count));
        } else {
            commentCount.setText(Integer.toString(count));
        }
    }

    private void disableSnapbyOptions() {
        snapbyInfoContainer.setVisibility(View.GONE);
        userContainer.setVisibility(View.GONE);

        imageView.setEnabled(false);
    }

    public void enableSnapbyOptions() {
        snapbyInfoContainer.setVisibility(View.VISIBLE);

        if (!type.equals("profile")) {
            userContainer.setVisibility(View.VISIBLE);
        }

        imageView.setEnabled(true);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (rootView == null) {
            return;
        }

        if (isVisibleToUser) {
            enableSnapbyOptions();
        } else {
            disableSnapbyOptions();
        }
    }
}