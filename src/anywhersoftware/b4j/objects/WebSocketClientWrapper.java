package anywhersoftware.b4j.objects;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.BA.DependsOn;
import anywheresoftware.b4a.BA.Events;
import anywheresoftware.b4a.BA.Hide;
import anywheresoftware.b4a.BA.ShortName;
import anywheresoftware.b4a.BA.Version;

/**
 * Implementation of a WebSocket client.
 */
@ShortName("WebSocketClient")
@Events(values={"Connected", "Closed (Reason As String)", "TextMessage (Message As String)"})
@DependsOn(values={"jetty_b4j"})
@Version(1.0f)
public class WebSocketClientWrapper {
	private BA ba;
	private String eventName;
	@Hide
	public WebSocketClient wsc;
	@Hide
	public Future<Session> session;
	/**
	 * Initializes the object and sets the subs that will handle the events.
	 */
	public void Initialize(BA ba, String EventName) {
		wsc = new WebSocketClient();
		eventName = EventName.toLowerCase(BA.cul);
		this.ba = ba;
	}
	/**
	 * Tries to connect to the given Url. The Url should start with ws://
	 */
	public void Connect(final String Url) {
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				try {
					wsc.start();
					URI echoUri = new URI(Url);
					ClientUpgradeRequest request = new ClientUpgradeRequest();
					session = wsc.connect(new WSHandler(), echoUri, request);
				} catch (Exception e) {
					
				}
			}
		};
		Thread t = new Thread(r);
		t.setDaemon(true);
		t.start();
	}
	/**
	 * Checks whether the connection is open.
	 */
	public boolean getConnected() throws InterruptedException {
		try {
			return session != null && session.isDone() && session.get().isOpen();
		} catch (ExecutionException e) {
			return false;
		}
	}
	/**
	 * Closes the connection.
	 */
	public void Close() throws Exception {
		if (session != null && session.isDone())
			session.get().close();
		if (wsc.isRunning()) {
			BA.submitRunnable(new Runnable() {
				
				@Override
				public void run() {
					try {
					wsc.stop();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
			}, null, 0);
			
		}
	}
	/**
	 * Sends a text message.
	 */
	public void SendText(String Text) throws IOException, InterruptedException, ExecutionException {
		session.get().getRemote().sendString(Text);
	}
	@Hide
	public class WSHandler extends WebSocketAdapter {
		@Override
		public void onWebSocketConnect(Session sess) {
			super.onWebSocketConnect(sess);
			ba.raiseEventFromDifferentThread(WebSocketClientWrapper.this, null, 0, eventName + "_connected", false, null);
		}
		@Override
		public void onWebSocketClose(int statusCode, String reason)
		{
			super.onWebSocketClose(statusCode, reason);
			ba.raiseEventFromDifferentThread(WebSocketClientWrapper.this, null, 0, eventName + "_closed", false, 
				new Object[] {BA.ReturnString(reason)});
		}
		@Override
		public void onWebSocketError(Throwable cause) {
			super.onWebSocketError(cause);
			cause.printStackTrace();
			try {
				Close();
				onWebSocketClose(0, cause.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		}
		@Override
		public void onWebSocketText(String message) {
			ba.raiseEventFromDifferentThread(WebSocketClientWrapper.this, null, 0, eventName + "_textmessage",
					false, new Object[] {message});
		}
		
	}
}
