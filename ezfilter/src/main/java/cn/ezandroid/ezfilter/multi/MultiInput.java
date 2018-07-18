package cn.ezandroid.ezfilter.multi;

import android.opengl.GLES20;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.GLRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;

/**
 * 多输入源渲染器
 *
 * @author like
 * @date 2018-07-13
 */
public class MultiInput extends FBORender {

    private int mNumOfInputs;
    private int[] mMultiTextureHandle;
    private int[] mMultiTexture;
    protected List<FBORender> mStartPointRenders;
    protected List<FBORender> mEndPointRenders;

    protected List<RenderPipeline> mRenderPipelines; // 每个输入源都生成一个渲染管道，便于对输入源各自添加滤镜

    public MultiInput(int numOfInputs) {
        super();
        this.mNumOfInputs = numOfInputs;
        mMultiTextureHandle = new int[numOfInputs - 1];
        mMultiTexture = new int[numOfInputs - 1];
        mStartPointRenders = new ArrayList<>(numOfInputs);
        mEndPointRenders = new ArrayList<>(numOfInputs);
        mRenderPipelines = new ArrayList<>(numOfInputs);
    }

    @Override
    public synchronized void onTextureAcceptable(int texture, GLRender source) {
        int pos = mEndPointRenders.lastIndexOf(source);
        if (pos <= 0) {
            mTextureIn = texture;
        } else {
            this.mMultiTexture[pos - 1] = texture;
        }
    }

    @Override
    protected void initShaderHandles() {
        super.initShaderHandles();
        for (int i = 0; i < mNumOfInputs - 1; i++) {
            mMultiTextureHandle[i] = GLES20.glGetUniformLocation(mProgramHandle,
                    UNIFORM_TEXTURE + (i + 2));
            // 从2开始：如inputImageTexture2，inputImageTexture3...
        }
    }

    @Override
    protected void bindShaderValues() {
        super.bindShaderValues();
        for (int i = 0; i < mNumOfInputs - 1; i++) {
            int tex = GLES20.GL_TEXTURE1 + i;
            GLES20.glActiveTexture(tex);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mMultiTexture[i]);
            GLES20.glUniform1i(mMultiTextureHandle[i], i + 1);
        }
    }

    public void clearRegisteredFilterLocations() {
        for (FBORender render : mEndPointRenders) {
            render.removeTarget(this);
        }
        mStartPointRenders.clear();
        mEndPointRenders.clear();
        mRenderPipelines.clear();
    }

    public void registerFilterLocation(FBORender filter) {
        if (!mStartPointRenders.contains(filter)) {
            FilterRender endRender = new FilterRender();
            endRender.addTarget(this);
            mStartPointRenders.add(filter);
            mEndPointRenders.add(endRender);
            RenderPipeline renderPipeline = new RenderPipeline();
            renderPipeline.onSurfaceCreated(null, null);
            renderPipeline.onSurfaceChanged(null, getWidth(), getHeight());
            renderPipeline.setStartPointRender(filter);
            renderPipeline.addEndPointRender(endRender);
            renderPipeline.startRender();
            mRenderPipelines.add(renderPipeline);
        }
    }

    public void registerFilterLocation(FBORender filter, int location) {
        int index = mStartPointRenders.indexOf(filter);
        if (index != -1) {
            mEndPointRenders.get(index).removeTarget(this);

            mStartPointRenders.remove(index);
            mEndPointRenders.remove(index);
            mRenderPipelines.remove(index);
        }
        FilterRender endRender = new FilterRender();
        endRender.addTarget(this);
        mStartPointRenders.add(location, filter);
        mEndPointRenders.add(location, endRender);
        RenderPipeline renderPipeline = new RenderPipeline();
        renderPipeline.onSurfaceCreated(null, null);
        renderPipeline.onSurfaceChanged(null, getWidth(), getHeight());
        renderPipeline.setStartPointRender(filter);
        renderPipeline.addEndPointRender(endRender);
        renderPipeline.startRender();
        mRenderPipelines.add(location, renderPipeline);
    }

    public List<FBORender> getStartPointRenders() {
        return mStartPointRenders;
    }

    public List<FBORender> getEndPointRenders() {
        return mEndPointRenders;
    }

    public List<RenderPipeline> getRenderPipelines() {
        return mRenderPipelines;
    }

    @Override
    public void onDrawFrame() {
        super.onDrawFrame();
        for (RenderPipeline pipeline : mRenderPipelines) {
            pipeline.onDrawFrame(null);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        for (RenderPipeline pipeline : mRenderPipelines) {
            pipeline.onSurfaceDestroyed();
        }
    }
}