close all;

path_figs = 'fig/';
path_data = 'data/';
data_file = ["igd","hv"];
y_label = ["IGD","HV"];

type = ["factory"];
xlabels = ["Factory Size","Number of Islands"];

colors = [ [0.9290 0.6940 0.1250];[0.8500 0.3250 0.0980];[0 0.4470 0.7410]];
colors1 = [[0 0 0]; [0 0 0]; [0 0 0]; [0 0 0]; [0 0 0]; [0 0 0]; [0 0 0]];
col_index = [1; 10; 11];


for type_index = 1:length(type)
    for qi = 1:length(data_file)
        f=figure('Position', [100, 100, 800, 400]);
        set(f,'defaultAxesColorOrder',[[0,60/255,255/255];[0,0,0]]);

        position = 1;
        for n = 1 : 8
           d = readmatrix(strcat(path_data, 'scale_',type(type_index),'_',data_file(qi),'_',string(n),'.txt'));
           color_index = 3;

           for c = 1 : length(col_index)
              col = col_index(c);
              data_col = d(:,col);

              boxplot(data_col, 'position', position, 'widths', 0.65, 'color', colors1(color_index,:));
              hold on;

              position = position + 1;
              color_index = color_index -1;
           end

           colordegree = 0.8;

            h = findobj(gca,'Tag','Box');
            patch(get(h(1),'XData'),get(h(1),'YData'), colors(1,:),'FaceAlpha',colordegree);
            patch(get(h(2),'XData'),get(h(2),'YData'), colors(2,:),'FaceAlpha',colordegree);
            patch(get(h(3),'XData'),get(h(3),'YData'), colors(3,:),'FaceAlpha',colordegree);

        end


        ylabel(strcat(y_label(qi),' rank'),'FontSize', 14)
        ylim([-0.8 11.5]);

        x_ticks = 2 : 3: 24;
        x_ticks_labels = 1:8;
        xlim([0 24.5]);
        set(gca,'xtick',x_ticks);
        set(gca,'xticklabel',x_ticks_labels,'FontSize', 12);
        xlabel(xlabels(type_index),'FontSize', 14 )

        c = get(gca, 'Children');
        hleg1 = legend(c(1:3),'MOEA/D', 'worst+diverse', 'linkage','Location','southeast','Orientation','horizontal','FontSize',14);
        saveas(gcf,strcat(path_figs,'scale_',type(type_index),'_',data_file(qi),'.eps'),'epsc');
        
    end
end

