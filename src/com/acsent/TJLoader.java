package com.acsent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

public class TJLoader {
/*
    class ParserTask implements Callable
    {
        Parser parser;
        BlockingQueue queue;

        ParserTask(Parser parser, BlockingQueue queue)
        {
            this.parser = parser;
            this.queue = queue;
        }

        public Object call()
        {
            while (parser.hasNextRecord())
            {
                try
                {
                    queue.put(parser.nextRecord());
                }
                catch (InterruptedException e)
                {
                    log.error("Failed to load feed.", e);
                    throw new RuntimeException("Failed to load feed.", e);
                }
            }
            parser.close();
            done = true; // Indicates that the parser is done.
            return null;
        }
    } */
    }
