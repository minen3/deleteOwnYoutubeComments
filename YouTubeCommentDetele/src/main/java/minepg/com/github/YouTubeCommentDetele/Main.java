package minepg.com.github.YouTubeCommentDetele;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelContentDetails;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentListResponse;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.api.services.youtube.model.PlaylistItemListResponse;

public class Main {
	private static final String CLIENT_SECRETS = "client_secret.json";
	private static final Collection<String> SCOPES = Arrays.asList("https://www.googleapis.com/auth/youtube.force-ssl");
	private static final String OPPOSITE_CHANNEL_ID = "[channelId]";
	private static final String OWN_CHANNEL_URL = "[http://www.youtube.com/channel/xxxxxxxxx]";//channelUrl
	private static final String APPLICATION_NAME = "deleteOwnYoutubeComments";
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

	/**
	 * Create an authorized Credential object.
	 *
	 * @return an authorized Credential object.
	 * @throws IOException
	 */

	public static Credential authorize(final NetHttpTransport httpTransport) throws IOException {
		// Load client secrets.
		InputStream in = Main.class.getResourceAsStream(CLIENT_SECRETS);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY,
				clientSecrets, SCOPES)
						.build();
		Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
		return credential;
	}

	/**
	 * Build and return an authorized API client service.
	 *
	 * @return an authorized API client service
	 * @throws GeneralSecurityException, IOException
	//	 */
	public static YouTube getService() throws GeneralSecurityException, IOException {
		final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		Credential credential = authorize(httpTransport);
		return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
				.setApplicationName(APPLICATION_NAME)
				.build();
	}

	/**
	 * Call function to create API service object. Define and
	 * execute API request. Print API response.
	 *
	 * @throws GeneralSecurityException, IOException, GoogleJsonResponseException
	 */

	public static String getPlaylistId(YouTube youtubeService) throws IOException {
		YouTube.Channels.List request = youtubeService.channels().list("contentDetails");
		ChannelListResponse response = request.setId(OPPOSITE_CHANNEL_ID).execute();
		List<Channel> responseItems = response.getItems();
		ChannelContentDetails responseContentDetails = responseItems.get(0).getContentDetails();
		return responseContentDetails.getRelatedPlaylists().getUploads();
	}

	public static List<String> getVideoId(YouTube youtubeService, String playlistId) throws IOException {
		List<String> videoId = new ArrayList<>();
		YouTube.PlaylistItems.List request = youtubeService.playlistItems().list("snippet");
		PlaylistItemListResponse response;
		do {
			response = request.setPlaylistId(playlistId).setMaxResults((long) 50).execute();

			for (int i = 0; i < response.getItems().size(); i++) {
				videoId.add(response.getItems().get(i).getSnippet().getResourceId().getVideoId());
			}
			System.out.println(videoId.get(videoId.size() - 1));
			request.setPageToken(response.getNextPageToken());
		} while (response.getNextPageToken() != null);

		return videoId;
	}

	public static void deleteComment(YouTube youtubeService, String id) throws IOException {
		YouTube.Comments.Delete request = youtubeService.comments().delete(id);
		request.execute();
	}

	public static List<String> getDeleteTargetComment(YouTube youtubeService, String videoId)
			throws IOException {
		List<String> targetCommentId = new ArrayList<>();
		YouTube.CommentThreads.List request = youtubeService.commentThreads().list("replies,snippet");
		CommentThreadListResponse response;

		do {
			try {
				response = request.setVideoId(videoId).setMaxResults((long) 100).execute();
			} catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
				break;
			}

			System.out.println("videoId" + videoId + "size : " + response.getItems().size());
			for (int i = 0; i < response.getItems().size(); i++) {

				//				ot(response, i);
				if (response.getItems().get(i).getSnippet().getTopLevelComment().getSnippet().getAuthorChannelUrl()
						.equals("http://www.youtube.com/channel/UCKVNFMnxB1QqJckidmdopWg")) {
					targetCommentId.add(response.getItems().get(i).getSnippet().getTopLevelComment().getId());
					System.out.println(
							response.getItems().get(i).getSnippet().getTopLevelComment().getSnippet().getTextDisplay());
				}
				Long totalReplyCount = response.getItems().get(i).getSnippet().getTotalReplyCount();

				if (totalReplyCount > 0) {
					if (response.getItems().get(i).getReplies() == null)
						continue;
					List<Comment> replies = response.getItems().get(i).getReplies().getComments();
					String parentId = replies.get(0).getSnippet().getParentId();

					YouTube.Comments.List replyRequest = youtubeService.comments().list("id,snippet");
					CommentListResponse replyResponse;
					do {
						replyResponse = replyRequest.setParentId(parentId).setMaxResults((long) 100).execute();
						for (int j = 0; j < replyResponse.getItems().size(); j++) {

							if (replyResponse.getItems().get(j).getSnippet().getAuthorChannelUrl()
									.equals("http://www.youtube.com/channel/UCKVNFMnxB1QqJckidmdopWg")) {

							}
						}
						replyRequest.setPageToken(replyResponse.getNextPageToken());
					} while (replyResponse.getNextPageToken() != null);
				}
			}
			request.setPageToken(response.getNextPageToken());
		} while (response.getNextPageToken() != null);

		return targetCommentId;
	}

	public static void main(String[] args)
			throws GeneralSecurityException, IOException, GoogleJsonResponseException {
		YouTube youtubeService = getService();
		String playlistId = getPlaylistId(youtubeService);

		List<String> videoId = getVideoId(youtubeService, playlistId);

		for (int i = 0; i < videoId.size(); i++) {
			List<String> dtc = getDeleteTargetComment(youtubeService, videoId.get(i));
			for (int j = 0; j < dtc.size(); j++) {
				deleteComment(youtubeService, dtc.get(j));
				System.out.println("delete " + dtc.get(j));
			}
		}
		/*
		 * YouTube.Comments.List request = youtubeService.comments();
		YouTube.Comments.Delete request = youtubeService.comments().delete(APPLICATION_NAME);
		
		request.execute();
		*/

	}
}
