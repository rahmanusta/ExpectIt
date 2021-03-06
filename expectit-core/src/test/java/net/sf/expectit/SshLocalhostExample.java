package net.sf.expectit;

/*
 * #%L
 * ExpectIt
 * %%
 * Copyright (C) 2014 Alexey Gavrilov and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static net.sf.expectit.matcher.Matchers.contains;
import static net.sf.expectit.matcher.Matchers.regexp;
import static net.sf.expectit.matcher.Matchers.times;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.IOException;
import java.util.Properties;

/**
 * An example of interacting with the local SSH server
 */
public class SshLocalhostExample {
    public static void main(String[] args) throws JSchException, IOException {
        JSch jSch = new JSch();
        Session session = jSch.getSession(System.getenv("USER"), "localhost");
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        jSch.addIdentity(System.getProperty("user.home") + "/.ssh/id_rsa");
        session.setConfig(config);
        session.connect();
        Channel channel = session.openChannel("shell");

        Expect expect = new ExpectBuilder()
                .withOutput(channel.getOutputStream())
                .withInputs(channel.getInputStream(), channel.getExtInputStream())
                .build();
        try {
            channel.connect();
            expect.expect(contains("$"));
            expect.sendLine("pwd");
            System.out.println(
                    "pwd1:" + expect.expect(times(2, contains("\n")))
                            .getResults()
                            .get(1)
                            .getBefore());
            expect.sendLine("pwd");
            // a regexp which captures the output of pwd
            System.out.println("pwd2:" + expect.expect(regexp("(?m)\\n([^\\n]*)\\n")).group(1));
            expect.expect(contains("$"));
            expect.sendLine("ls -l");
            // skipping the echo command
            expect.expect(times(2, contains("\n")));
            // getting the output of ls
            System.out.println(expect.expect(regexp(".*\\$")).getBefore().trim());
            expect.sendLine("exit");
        } finally {
            channel.disconnect();
            session.disconnect();
            expect.close();
        }
    }
}
